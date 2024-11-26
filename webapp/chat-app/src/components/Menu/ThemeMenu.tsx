import React from 'react';
import styled from 'styled-components';
import {useTheme} from '../../hooks/useTheme';
import {themes} from '../../themes/themes';

const ThemeMenuContainer = styled.div`
    position: relative;
    display: inline-block;
`;

const ThemeButton = styled.button`
    padding: ${({theme}) => theme.sizing.spacing.sm};
    color: ${({theme}) => theme.colors.text.primary};
    background: ${({theme}) => theme.colors.surface};
    border: 1px solid ${({theme}) => theme.colors.border};
    border-radius: ${({theme}) => theme.sizing.borderRadius.sm};

    &:hover {
        background: ${({theme}) => theme.colors.primary};
        color: ${({theme}) => theme.colors.background};
    }
`;

const ThemeList = styled.div`
    position: absolute;
    top: 100%;
    right: 0;
    background: ${({theme}) => theme.colors.surface};
    border: 1px solid ${({theme}) => theme.colors.border};
    border-radius: ${({theme}) => theme.sizing.borderRadius.sm};
    padding: ${({theme}) => theme.sizing.spacing.xs};
    z-index: 10;
    min-width: 150px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
`;

const ThemeOption = styled.button`
    width: 100%;
    padding: ${({theme}) => theme.sizing.spacing.sm};
    text-align: left;
    color: ${({theme}) => theme.colors.text.primary};
    background: none;
    border: none;
    border-radius: ${({theme}) => theme.sizing.borderRadius.sm};

    &:hover {
        background: ${({theme}) => theme.colors.primary};
        color: ${({theme}) => theme.colors.background};
    }
`;

export const ThemeMenu: React.FC = () => {
    const [currentTheme, setTheme] = useTheme();
    const [isOpen, setIsOpen] = React.useState(false);

    const handleThemeChange = (themeName: keyof typeof themes) => {
        setTheme(themeName);
        setIsOpen(false);
    };

    return (
        <ThemeMenuContainer>
            <ThemeButton onClick={() => setIsOpen(!isOpen)}>
                Theme: {currentTheme}
            </ThemeButton>
            {isOpen && (
                <ThemeList>
                    {Object.keys(themes).map((themeName) => (
                        <ThemeOption
                            key={themeName}
                            onClick={() => handleThemeChange(themeName as keyof typeof themes)}
                        >
                            {themeName}
                        </ThemeOption>
                    ))}
                </ThemeList>
            )}
        </ThemeMenuContainer>
    );
};
